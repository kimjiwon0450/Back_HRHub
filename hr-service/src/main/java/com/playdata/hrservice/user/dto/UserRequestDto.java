package com.playdata.hrservice.user.dto;

import com.playdata.hrservice.user.entity.User;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserRequestDto {
    private Long id;
    private MultipartFile profileImage;

    public User toEntity() {
        return User.builder()
                .id(id)
                .build();
    }

}
