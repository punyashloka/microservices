package com.punyam.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class HealthCheck implements HealthIndicator {
  @Autowired
  private Environment environment;
    @Override
    public Health health() {
        int errorCode = check(); // perform some specific health check
        if (errorCode != 0) {
            return Health.down()
              .withDetail("Error Code", errorCode).build();
        }
        return Health.up().withDetail("Server Port" , environment.getProperty("local.server.port")).build();
    }
     
    public int check() {
        // Our logic to check health
        return 0;
    }
}
