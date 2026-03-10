package com.lk.datamarket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lk.datamarket.mapper")
public class DataMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataMarketApplication.class, args);
    }

}
