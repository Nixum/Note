import org.junit.Test;

/**
 * 单例模式
 */
public class Singleton {

    @Test
    public void testSingleton(){

        for(int i=0; i<200; i++) {
            final int t = i;
            new Thread(new Runnable() {
                public void run() {
//                    DoubleCheckedLocking s = DoubleCheckedLocking.getInstance();
//                    LazyInstance s = LazyInstance.getInstance();
//                    HungryInstance s = HungryInstance.getInstance();
                    StaticInnerClassMode s = StaticInnerClassMode.getInstance();
//                    EnumMode s = EnumMode.SINGLETON;
                    System.out.println("thread-" + t + " " + s.hashCode());
                }
            }).start();
        }
    }
}

/** 1.
 * 懒汉模式,线程不安全，只有在调用方法的时候才实例化,好处是没用到该类时就不实例化，节约资源
 */
class LazyInstance {

    private static LazyInstance singleton;

    private LazyInstance() {
//        if (singleton != null)
//            throw new RuntimeException();
    }

    /** 1.1
     * 想要线程安全只需在方法上加上synchronized关键字，缺点是，多线程访问时锁的操作耗时
     * public synchronized static LazyInstance getInstance()
     * public static synchronized LazyInstance getInstance() synchronized写在static前后都可以
     */
    public static LazyInstance getInstance() {
        if (singleton == null) {
            singleton = new LazyInstance();
        }
        return singleton;
    }

}

/** 2.
 * 饿汉模式，直接实例化，线程安全，缺点是丢失了延迟实例化造成资源浪费
 */
class HungryInstance {

    private static final HungryInstance singleton = new HungryInstance();   //加不加final都可以

    public static HungryInstance getInstance() {
        return singleton;
    }

}

/** 3.
 * 双重锁,可在多线程下使用
 */
class DoubleCheckedLocking {

    /**
     * 注意变量要声明volatile,也需要两次if判断,否则可能因为指令重排序导致在多线程情况下不安全,这个比较难测试
     * singleton = new Singleton()不是原子操作，而分为了三个步骤
     * 1. 给 singleton 分配内存
     * 2. 调用 Singleton 的构造函数来初始化成员变量，形成实例
     * 3. 将singleton对象指向分配的内存空间（执行完这步 singleton才是非 null了）
     * 由于有一个『instance已经不为null但是仍没有完成初始化』的中间状态，而这个时候，
     * 如果有其他线程刚好运行到第一层if (instance ==null)这里，这里读取到的instance已经不为null了，
     * 所以就直接把这个中间状态的instance拿去用了，就会产生问题。这里的关键在于线程T1对instance的写操作没有完成，
     * 线程T2就执行了读操作 **/
    private volatile static DoubleCheckedLocking singleton;

    public static DoubleCheckedLocking getInstance(){
        if (singleton == null) {
            synchronized (DoubleCheckedLocking.class) {
                if (singleton == null) {
                    singleton = new DoubleCheckedLocking();
                }
            }
        }
        return singleton;
    }

}

/**4.
 * 静态内部类模式，利用的是JVM对静态内部类的加载机制
 * 因为静态内部类只有被调用的时候才会被初始化，相当于延时的机制，且JVM能保证只初始化一次
 * 相当与结合了懒汉模式和饿汉模式的优点吧
 */
class StaticInnerClassMode {

    private static class StaticInnerClassInstance {
        private static final StaticInnerClassMode SINGLETON = new StaticInnerClassMode();
    }

    public static StaticInnerClassMode getInstance() {
        return StaticInnerClassInstance.SINGLETON;
    }
}

/**5.
 * 枚举类创建单例,利用JVM的机制,保证只实例化一次,同时可防止反射和反序列化操作破解
 */
enum EnumMode {
    SINGLETON;
    public void method(){}
}

/**
 * 除了枚举类可防止反射和反序列化操作破解外，其他四种方法都会被反射和反序列化破解
 * 1，阻止反射破解
 * 在空构造方法里，判断singleton是否为空，如果不为空，则抛出RuntimeException，
 * 因为反射需要通过class.getInstance()调用空参构造方法实例化对象，如果此时抛出异常，则会终止程序，
 * 如果在懒汉模式里使用就会发现会抛出异常
 *
 * 2.阻止反序列化破解
 *  实现Serializable接口，定义readResolve()方法返回对象，具体原理不太清楚
 *  在反序列化的时候用readResolve()中返回的对象直接替换在反序列化过程中创建的对象
 *  private Object readResolve() throws ObjectStreamException {
 *     return instance;
 *  }
 */