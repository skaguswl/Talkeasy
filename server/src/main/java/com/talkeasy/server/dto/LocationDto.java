package com.talkeasy.server.dto;

import com.talkeasy.server.domain.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto implements Serializable {
    String email;
    String x, y;

    public Location toEntity(){
        return Location.builder().email(email).x(x).y(y).build();
    }
}