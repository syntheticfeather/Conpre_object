package JavaStudy.Reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.StringJoiner;

public class L {
    public void test() throws Exception {
        // 对strin文件夹种StringJoiner部分的反射重写
        // 使用反射创建 StringJoiner 的类对象
        Class<?> sjClass = Class.forName("java.util.StringJoiner");

        // 创建第一个 StringJoiner (sj1) 类的实例对象
        Constructor<?> constructor = sjClass.getConstructor(CharSequence.class);
        Object sj = constructor.newInstance(", ");

        // 反射调用 add 方法， 先拿 method 方法的实例对象，再调用
        Method addMethod = sjClass.getMethod("add", CharSequence.class);
        addMethod.invoke(sj, "First");

        String[] strs = { "Hello", "World", "Java" };
        for (String str : strs) {
            addMethod.invoke(sj, str);
        }
        addMethod.invoke(sj, "!");

        // 反射调用 toString
        Method toStringMethod = sjClass.getMethod("toString");
        System.out.println(toStringMethod.invoke(sj));
        System.out.println(toStringMethod.invoke(sj));

        // 创建带前缀后缀的 StringJoiner (sj1)
        Constructor<?> fullConstructor = sjClass.getConstructor(CharSequence.class, CharSequence.class,
                CharSequence.class);
        Object sj1 = fullConstructor.newInstance(", ", "[", "]");
        addMethod.invoke(sj1, "A");
        addMethod.invoke(sj1, "B");

        // 创建第二个 StringJoiner (sj2)
        Object sj2 = fullConstructor.newInstance("-", "{", "}");
        addMethod.invoke(sj2, "C");
        addMethod.invoke(sj2, "D");

        // 反射调用 merge 方法
        Method mergeMethod = sjClass.getMethod("merge", StringJoiner.class);
        mergeMethod.invoke(sj1, sj2);
        System.out.println(toStringMethod.invoke(sj1)); // [A, B, C, D]

        mergeMethod.invoke(sj2, sj1);
        System.out.println(toStringMethod.invoke(sj2)); // {A, B, C, D}
    }

    public static void main(String[] args) throws Exception {
        new L().test();

    }

}
