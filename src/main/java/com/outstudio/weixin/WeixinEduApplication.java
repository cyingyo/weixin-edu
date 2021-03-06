package com.outstudio.weixin;

import com.outstudio.weixin.common.dao.UserEntityMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
@MapperScan(basePackageClasses = UserEntityMapper.class)
public class WeixinEduApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeixinEduApplication.class, args);
	}
}
