package com.borjasanba.bikes.client;

import com.borjasanba.bikes.model.StationWrapper;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(value = "citibikenycClient", url = "${bike.nyc.url}")
public interface CityBikeStationClient {

    @RequestMapping(method = RequestMethod.GET, value = "/gbfs/en/station_information.json")
    StationWrapper getStationInfo();

    @RequestMapping(method = RequestMethod.GET, value = "/gbfs/en/station_status.json")
    StationWrapper getStationStatus();

}
