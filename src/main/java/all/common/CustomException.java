package all.common;

public class CustomException extends Exception{
    // 提供一个有参数的构造方法，可自动生成
    public CustomException(String message) {
        super(message);// 把参数传递给Throwable的带String参数的构造方法
    }
}
