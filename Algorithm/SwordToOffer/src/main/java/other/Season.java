package other;

public enum Season{
    // 调用无参构造器
    SPRING() {
        // 方法无法调用enum类里的非静态变量，只能调用非静态变量
        @Override
        public String whatSeason(){return "chun";}
    },
    // 调用有参构造器
    SUMMER("夏天") {
        @Override
        public String whatSeason(){return "xia";}
    },
    // 默认调用无参构造器
    FALL {
        @Override
        public String whatSeason(){return "qiu";}
    };

    public String name;

    Season() {}

    private Season(String name) {
        this.name = name;
    }

    public abstract String whatSeason();
}