# smart-customer-service/utils/repayment_calculator.py
from decimal import Decimal, ROUND_HALF_UP
from typing import List, Dict
from enum import Enum

class RepaidType(Enum):
    EQUAL_PRINCIPAL_AND_INTEREST = "等额本息"
    EQUAL_PRINCIPAL = "等额本金"
    INTEREST_FIRST = "先息后本"
    ONE_TIME = "一次性还本付息"

class RepaymentCalculator:
    @staticmethod
    def _round_money(amount: Decimal) -> Decimal:
        return amount.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    @staticmethod
    def _round_rate(amount: Decimal) -> Decimal:
        return amount.quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)

    @staticmethod
    def calculate(repaid_type: str, loan_amount: Decimal, loan_period: int, interest_rate: Decimal) -> Dict:
        repaid_type_enum = RepaymentCalculator._get_repaid_type(repaid_type)
        if repaid_type_enum is None:
            raise ValueError(f"不支持的还款方式: {repaid_type}")

        if repaid_type_enum == RepaidType.EQUAL_PRINCIPAL_AND_INTEREST:
            return RepaymentCalculator._calculate_equal_principal_and_interest(loan_amount, loan_period, interest_rate)
        elif repaid_type_enum == RepaidType.EQUAL_PRINCIPAL:
            return RepaymentCalculator._calculate_equal_principal(loan_amount, loan_period, interest_rate)
        elif repaid_type_enum == RepaidType.INTEREST_FIRST:
            return RepaymentCalculator._calculate_interest_first(loan_amount, loan_period, interest_rate)
        elif repaid_type_enum == RepaidType.ONE_TIME:
            return RepaymentCalculator._calculate_one_time(loan_amount, loan_period, interest_rate)

    @staticmethod
    def _get_repaid_type(repaid_type: str) -> RepaidType:
        type_mapping = {
            "等额本息": RepaidType.EQUAL_PRINCIPAL_AND_INTEREST,
            "等额本金": RepaidType.EQUAL_PRINCIPAL,
            "先息后本": RepaidType.INTEREST_FIRST,
            "一次性还本付息": RepaidType.ONE_TIME,
        }
        return type_mapping.get(repaid_type)

    @staticmethod
    def _calculate_equal_principal_and_interest(loan_amount: Decimal, loan_period: int, interest_rate: Decimal) -> Dict:
        monthly_rate = interest_rate / 12
        if monthly_rate == 0:
            monthly_payment = loan_amount / loan_period
            total_payment = loan_amount
        else:
            monthly_payment = loan_amount * monthly_rate * (1 + monthly_rate) ** loan_period / ((1 + monthly_rate) ** loan_period - 1)
            total_payment = monthly_payment * loan_period

        monthly_payment = RepaymentCalculator._round_money(monthly_payment)
        total_payment = RepaymentCalculator._round_money(total_payment)

        repayment_plan = []
        remaining_principal = loan_amount

        for term in range(1, loan_period + 1):
            interest = RepaymentCalculator._round_money(remaining_principal * monthly_rate)
            principal = monthly_payment - interest
            remaining_principal -= principal

            if term == loan_period:
                principal = RepaymentCalculator._round_money(remaining_principal + principal)
                remaining_principal = Decimal("0")

            repayment_plan.append({
                "term": term,
                "principal": str(principal),
                "interest": str(interest),
                "total": str(principal + interest)
            })

        total_interest = total_payment - loan_amount

        return {
            "monthlyPayment": str(monthly_payment),
            "totalPayment": str(total_payment),
            "totalInterest": str(RepaymentCalculator._round_money(total_interest)),
            "repaymentPlan": repayment_plan
        }

    @staticmethod
    def _calculate_equal_principal(loan_amount: Decimal, loan_period: int, interest_rate: Decimal) -> Dict:
        monthly_rate = interest_rate / 12
        monthly_principal = RepaymentCalculator._round_money(loan_amount / loan_period)

        repayment_plan = []
        remaining_principal = loan_amount
        total_interest = Decimal("0")

        for term in range(1, loan_period + 1):
            interest = RepaymentCalculator._round_money(remaining_principal * monthly_rate)
            total_interest += interest
            principal = monthly_principal

            if term == loan_period:
                principal = RepaymentCalculator._round_money(remaining_principal)

            remaining_principal -= principal

            repayment_plan.append({
                "term": term,
                "principal": str(principal),
                "interest": str(interest),
                "total": str(principal + interest)
            })

        total_payment = loan_amount + total_interest
        first_month_payment = monthly_principal + RepaymentCalculator._round_money(loan_amount * monthly_rate)

        return {
            "monthlyPayment": str(first_month_payment),
            "totalPayment": str(RepaymentCalculator._round_money(total_payment)),
            "totalInterest": str(RepaymentCalculator._round_money(total_interest)),
            "repaymentPlan": repayment_plan
        }

    @staticmethod
    def _calculate_interest_first(loan_amount: Decimal, loan_period: int, interest_rate: Decimal) -> Dict:
        monthly_rate = interest_rate / 12
        monthly_interest = RepaymentCalculator._round_money(loan_amount * monthly_rate)
        total_interest = RepaymentCalculator._round_money(monthly_interest * loan_period)

        repayment_plan = []
        for term in range(1, loan_period):
            repayment_plan.append({
                "term": term,
                "principal": "0",
                "interest": str(monthly_interest),
                "total": str(monthly_interest)
            })

        repayment_plan.append({
            "term": loan_period,
            "principal": str(loan_amount),
            "interest": str(monthly_interest),
            "total": str(loan_amount + monthly_interest)
        })

        return {
            "monthlyPayment": str(monthly_interest),
            "totalPayment": str(RepaymentCalculator._round_money(loan_amount + total_interest)),
            "totalInterest": str(total_interest),
            "repaymentPlan": repayment_plan
        }

    @staticmethod
    def _calculate_one_time(loan_amount: Decimal, loan_period: int, interest_rate: Decimal) -> Dict:
        monthly_rate = interest_rate / 12
        total_interest = RepaymentCalculator._round_money(loan_amount * monthly_rate * loan_period)
        total_payment = RepaymentCalculator._round_money(loan_amount + total_interest)

        repayment_plan = [{
            "term": loan_period,
            "principal": str(loan_amount),
            "interest": str(total_interest),
            "total": str(total_payment)
        }]

        return {
            "monthlyPayment": str(total_payment),
            "totalPayment": str(total_payment),
            "totalInterest": str(total_interest),
            "repaymentPlan": repayment_plan
        }
