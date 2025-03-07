package io.metersphere.api.jmeter;

import io.metersphere.api.jmeter.queue.ExecThreadPoolExecutor;
import io.metersphere.api.jmeter.utils.CommonBeanFactory;
import io.metersphere.api.jmeter.utils.FixedCapacityUtils;
import io.metersphere.api.jmeter.utils.JmeterProperties;
import io.metersphere.api.jmeter.utils.MSException;
import io.metersphere.dto.JmeterRunRequestDTO;
import io.metersphere.jmeter.JMeterBase;
import io.metersphere.jmeter.LocalRunner;
import io.metersphere.utils.LoggerUtil;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;

@Service
public class JMeterService {

    @Resource
    private JmeterProperties jmeterProperties;

    @PostConstruct
    public void init() {
        String JMETER_HOME = getJmeterHome();
        String JMETER_PROPERTIES = JMETER_HOME + "/bin/jmeter.properties";
        JMeterUtils.loadJMeterProperties(JMETER_PROPERTIES);
        JMeterUtils.setJMeterHome(JMETER_HOME);
        JMeterUtils.setLocale(LocaleContextHolder.getLocale());
    }

    public String getJmeterHome() {
        String home = getClass().getResource("/").getPath() + "jmeter";
        try {
            File file = new File(home);
            if (file.exists()) {
                return home;
            } else {
                return jmeterProperties.getHome();
            }
        } catch (Exception e) {
            return jmeterProperties.getHome();
        }
    }

    public void runLocal(JmeterRunRequestDTO runRequest, HashTree testPlan) {
        try {
            init();
            if (!FixedCapacityUtils.jmeterLogTask.containsKey(runRequest.getReportId())) {
                FixedCapacityUtils.jmeterLogTask.put(runRequest.getReportId(), System.currentTimeMillis());
            }
            runRequest.setHashTree(testPlan);
            JMeterBase.addSyncListener(runRequest, runRequest.getHashTree(), APISingleResultListener.class.getCanonicalName());
            LocalRunner runner = new LocalRunner(testPlan);
            runner.run(runRequest.getReportId());
        } catch (Exception e) {
            LoggerUtil.error(e.getMessage(), e);
            MSException.throwException("读取脚本失败");
        }
    }

    public void run(JmeterRunRequestDTO request) {
        if (request.getCorePoolSize() > 0) {
            CommonBeanFactory.getBean(ExecThreadPoolExecutor.class).setCorePoolSize(request.getCorePoolSize());
        }
        CommonBeanFactory.getBean(ExecThreadPoolExecutor.class).addTask(request);
    }

    public void addQueue(JmeterRunRequestDTO request) {
        this.runLocal(request, request.getHashTree());
    }
}
