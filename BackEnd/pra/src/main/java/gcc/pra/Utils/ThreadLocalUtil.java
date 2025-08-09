package gcc.pra.Utils;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class ThreadLocalUtil {

    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

    public static <T> T get() {
        return (T) THREAD_LOCAL.get();
    }

    public static void set(Object value) {
        THREAD_LOCAL.set(value);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public static Integer getId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return (Integer) map.get("id");
    }
}
