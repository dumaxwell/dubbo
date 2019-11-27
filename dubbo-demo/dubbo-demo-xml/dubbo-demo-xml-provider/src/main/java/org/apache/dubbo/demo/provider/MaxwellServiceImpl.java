package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.demo.MaxwellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Auther: semaxwell
 * Time: 2019-11-26 10:31
 **/

public class MaxwellServiceImpl implements MaxwellService {

    DemoService demoService;

    public DemoService getDemoService() {
        return demoService;
    }

    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }

    @Override
    public String invokeDemoSayHello(String name) {
        return demoService.sayHello(name);
//        return "maxwell service " + name;
    }
}
