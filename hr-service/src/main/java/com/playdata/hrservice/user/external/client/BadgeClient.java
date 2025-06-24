package com.playdata.hrservice.user.external.client;

import com.playdata.hrservice.user.dto.UserBadgeResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "point-service" )
public interface BadgeClient {
    @GetMapping("/badges/user/{userId}")
    UserBadgeResDto getUserBadge(@PathVariable("userId") Long userId);



}
