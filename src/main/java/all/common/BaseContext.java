package all.common;

/**
 * 通过ThreadLocal来获取当前线程操作用户的id
 */
public class BaseContext {
    private static ThreadLocal threadLocal = new ThreadLocal<>();

    /**
     * 把当前线程的用户id设置给threadLocal
     * @param id
     */
    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    /**
     * 获取当前线程的用户id
     * @return
     */
    public static Long getCurrentId(){
        return (Long) threadLocal.get();
    }

}
