package cn.com.mfish.scheduler.invoke;

import cn.com.mfish.scheduler.common.InvokeUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @description: 远程RPC调用任务(实现类写在具体服务中, 通过feign接口访问)
 * @author: mfish
 * @date: 2023/2/13 16:46
 */
@Slf4j
public class RPCInvoke implements BaseInvoke {

    /**
     * RPC调度尽量不要调用时间过长的任务，一般用于服务直接简短通知
     *
     * @param className
     * @param methodName
     * @param params
     * @return
     */
    @Override
    public <T> Object run(String className, String methodName, List<T> params) {
        try {
            Object obj = InvokeUtils.invokeFeignMethod(className, methodName, params);
            log.info("返回结果:" + obj);
            return obj;
        } catch (Exception e) {
             log.error("任务执行出错", e);
            return null;
        }
    }

}
