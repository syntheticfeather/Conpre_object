# Lambda表达式

## 格式

(parameters)-> {statements;}

- parameters:方法的参数列表，可以显示写出，也可以由编译器推断（？
- statements:Lambda的主体，只有一条语句可以省略{}，该语句自动成为返回值。多条语句如果有返回值，必须用return

``` java
interface MathOperation {
    int operate(int a, int b);
}

// 匿名内部类

MathOperation add = new MathOperation() { 
    @Override
    public int operate(int a, int b) { 
        return a + b; 
        }
    };

// Lambda写法

MathOperation add = (a, b) -> a + b;
```

## 适用条件

函数式接口（只有**一个抽象方法**的接口）  
`@FunctionInterface`标识这种接口

## 常用的函数式接口

- Predicate<T>：接受一个参数，返回一个布尔值。 (T) -> boolean

- Function<T, R>：接受一个参数，返回一个结果。 (T) -> R

- Consumer<T>：接受一个参数，不返回结果。 (T) -> void

- Supplier<T>：不接受参数，返回一个结果。 () -> T

## 方法引用

Lambda 表达式的一种更简洁的写法。当只是调用一个已经存在的方法时，可以用方法引用替代 Lambda 表达式

### 格式

类名/对象::方法名

|Lambda表达式|方法引用|说明|
|---|---|---|
|s -> System.out.println(s)|System.out::println|System.out对象的println方法|