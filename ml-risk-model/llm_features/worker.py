"""L7.2 风控 Mock Worker。

消费 ``risk.job.created``，调用（模拟）LLM 特征提取，
**只输出 G/H 特征**到 ``risk.job.completed``。
最终违约概率由 Java 端 ``FullRiskScoringService`` 计算，Worker 端不打分。

用法::

    pip install -r requirements.txt
    python -m llm_features.worker

环境变量：
    RISK_MQ_URL                 默认 amqp://guest:guest@127.0.0.1:5672/%2F
    RISK_EXCHANGE               默认 risk.exchange
    RISK_JOB_CREATED_QUEUE      默认 risk.job.created.queue
    RISK_JOB_CREATED_ROUTING_KEY 默认 risk.job.created
    RISK_JOB_COMPLETED_ROUTING_KEY 默认 risk.job.completed
    RISK_JOB_CREATED_RETRY_ROUTING_KEY 默认 risk.job.created.retry
    RISK_MOCK_DELAY_SECONDS     默认 0（模拟计算耗时）
"""
from __future__ import annotations

import json
import os
import time
from typing import Any, Dict

import pika

from llm_features.config import load_env
from llm_features.schema import DETERMINISTIC_FEATURE_NAMES, default_feature_dict

# 先加载 .env，再读取配置（已存在的 shell 环境变量优先）
load_env()

MQ_URL = os.getenv("RISK_MQ_URL", "amqp://guest:guest@127.0.0.1:5672/%2F")
EXCHANGE = os.getenv("RISK_EXCHANGE", "risk.exchange")
CREATED_QUEUE = os.getenv("RISK_JOB_CREATED_QUEUE", "risk.job.created.queue")
CREATED_ROUTING_KEY = os.getenv("RISK_JOB_CREATED_ROUTING_KEY", "risk.job.created")
COMPLETED_ROUTING_KEY = os.getenv("RISK_JOB_COMPLETED_ROUTING_KEY", "risk.job.completed")
CREATED_RETRY_ROUTING_KEY = os.getenv(
    "RISK_JOB_CREATED_RETRY_ROUTING_KEY", "risk.job.created.retry"
)
CREATED_RETRY_QUEUE = os.getenv(
    "RISK_JOB_CREATED_RETRY_QUEUE", "risk.job.created.retry.queue"
)
DLX = os.getenv("RISK_DLX", "dlx")
DLQ_ROUTING_KEY = os.getenv("RISK_DLQ_ROUTING_KEY", "dlx-dead-letter-routing-key")
MOCK_DELAY_SECONDS = float(os.getenv("RISK_MOCK_DELAY_SECONDS", "0"))

# G 组：与 llm_features.schema.LLM_FEATURE_NAMES / Java FULL_FEATURE_NAMES 顺序对齐
MOCK_FEATURES: Dict[str, float] = {
    "llm_verdict": 1.0,
    "llm_confidence": 0.86,
    "doc_income_consistency": 1.0,
    "doc_income_stability": 0.8,
    "doc_employment_risk": 0.0,
    "doc_multi_lender_signal": 0.1,
    "doc_material_mismatch": 0.0,
    "doc_suspicious_document": 0.0,
    "doc_debt_pressure": 0.2,
    "doc_negative_public_record": 0.0,
    "doc_missing_key_info": 0.0,
    "llm_feature_available": 1.0,
}

# H 组：与 llm_features.schema.DETERMINISTIC_FEATURE_NAMES 对齐
MOCK_DETERMINISTIC: Dict[str, float] = {
    "doc_total_chars": 2400.0,
    "doc_count": 5.0,
    "doc_type_coverage": 0.83,
    "note_length": 0.0,
    "has_ocr_text": 1.0,
}


def _empty_deterministic() -> Dict[str, float]:
    return {name: 0.0 for name in DETERMINISTIC_FEATURE_NAMES}


def build_completed_event(
    created: Dict[str, Any], status: str = "ok", error_code: str | None = None
) -> Dict[str, Any]:
    """由创建事件生成完成事件（仅含 G/H 特征，不含分数）。"""
    ok = status == "ok"
    return {
        "event": "RISK_JOB_COMPLETED",
        "job_id": created.get("job_id"),
        "application_id": created.get("application_id"),
        "user_id": created.get("user_id"),
        "job_version": created.get("job_version", 1),
        "status": status,
        "error_code": error_code,
        "features": dict(MOCK_FEATURES) if ok else default_feature_dict(available=False),
        "deterministic_features": dict(MOCK_DETERMINISTIC) if ok else _empty_deterministic(),
        "prompt_version": "mock_v1",
        "converter_version": "mock_v1",
        "model": "mock-worker",
        "cached": False,
        "reasoning": "L7.2 Mock Worker 固定输出（仅 G/H 特征，无分数）",
    }


def _publish_completed(
    channel: pika.channel.Channel, completed: Dict[str, Any], job_id: Any
) -> None:
    channel.basic_publish(
        exchange=EXCHANGE,
        routing_key=COMPLETED_ROUTING_KEY,
        body=json.dumps(completed, ensure_ascii=False).encode("utf-8"),
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2,
            headers={"messageId": f"risk_job_completed_{job_id}"},
        ),
    )


def handle_message(
    channel: pika.channel.Channel,
    method: pika.spec.Basic.Deliver,
    properties: pika.spec.BasicProperties,
    body: bytes,
) -> None:
    created: Dict[str, Any] = {}
    try:
        created = json.loads(body.decode("utf-8"))
        job_id = created.get("job_id")
        if job_id is None:
            raise ValueError("创建事件缺少 job_id")

        if MOCK_DELAY_SECONDS > 0:
            time.sleep(MOCK_DELAY_SECONDS)

        completed = build_completed_event(created)
        _publish_completed(channel, completed, job_id)
        channel.basic_ack(delivery_tag=method.delivery_tag)
        print(f"[worker] job_id={job_id} 已输出 G/H 特征并回传", flush=True)
    except Exception as exc:  # noqa: BLE001 - 任何异常都转为 error 完成事件，避免无限重试
        print(f"[worker] 处理失败: {exc}", flush=True)
        job_id = created.get("job_id") if isinstance(created, dict) else None
        try:
            completed = build_completed_event(
                created, status="error", error_code="WORKER_EXCEPTION"
            )
            _publish_completed(channel, completed, job_id or "unknown")
            channel.basic_ack(delivery_tag=method.delivery_tag)
            print(f"[worker] job_id={job_id} 已回传 error 完成事件", flush=True)
        except Exception as pub_exc:  # noqa: BLE001
            # 连完成事件都发不出去时，按原重试/DLQ 机制处理
            print(f"[worker] error 完成事件发送失败: {pub_exc}", flush=True)
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def declare_topology(channel: pika.channel.Channel) -> None:
    """声明与 Java 端一致的交换机 / 队列参数，保证独立启动也可用。"""
    channel.exchange_declare(exchange=EXCHANGE, exchange_type="topic", durable=True)
    channel.queue_declare(
        queue=CREATED_QUEUE,
        durable=True,
        arguments={
            "x-dead-letter-exchange": DLX,
            "x-dead-letter-routing-key": CREATED_RETRY_ROUTING_KEY,
        },
    )
    channel.queue_bind(
        queue=CREATED_QUEUE, exchange=EXCHANGE, routing_key=CREATED_ROUTING_KEY
    )
    # 声明重试队列，保持与 Java 拓扑一致（当前 Worker 对业务异常直接 ACK，重试队列兜底网络抖动）
    channel.queue_declare(
        queue=CREATED_RETRY_QUEUE,
        durable=True,
        arguments={
            "x-message-ttl": 10000,
            "x-dead-letter-exchange": EXCHANGE,
            "x-dead-letter-routing-key": CREATED_ROUTING_KEY,
        },
    )
    channel.queue_bind(
        queue=CREATED_RETRY_QUEUE, exchange=DLX, routing_key=CREATED_RETRY_ROUTING_KEY
    )


def main() -> None:
    params = pika.URLParameters(MQ_URL)
    params.heartbeat = 60
    while True:
        try:
            connection = pika.BlockingConnection(params)
            channel = connection.channel()
            declare_topology(channel)
            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(
                queue=CREATED_QUEUE,
                on_message_callback=handle_message,
                auto_ack=False,
            )
            print(f"[worker] 已启动，监听 {CREATED_QUEUE}（仅输出 G/H 特征）", flush=True)
            channel.start_consuming()
        except KeyboardInterrupt:
            print("[worker] 收到退出信号", flush=True)
            break
        except Exception as exc:  # noqa: BLE001 - 断线后 3 秒重连
            print(f"[worker] 连接异常，3 秒后重试: {exc}", flush=True)
            time.sleep(3)


if __name__ == "__main__":
    main()
