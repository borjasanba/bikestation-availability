package com.borjasanba.bikes.connector;

import com.borjasanba.bikes.client.CityBikeStationClient;
import com.borjasanba.bikes.producer.CityBikesProducer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableFeignClients(basePackages = "com.borjasanba.bikes.client")
public class CityBikesStationConnector {

    @NonNull
    private CityBikeStationClient citiBikeStationClient;

    @NonNull
    private CityBikesProducer cityBikesProducer;

    @Scheduled(fixedDelayString = "${connector.station.info.rate}")
    public void sendStationInfo() {
        citiBikeStationClient.getStationInfo().getData().getStations()
                .forEach(stationInfo -> {
                    log.debug("sending station info {}", stationInfo);
                    cityBikesProducer.sendStationInfo(stationInfo);
                });
    }

    @Scheduled(fixedDelayString = "${connector.station.status.rate}")
    public void sendStationStatus() {
        citiBikeStationClient.getStationStatus().getData().getStations()
                .forEach(stationStatus -> {
                    log.debug("sending station status {}", stationStatus);
                    cityBikesProducer.sendStationStatus(stationStatus);
                });
    }

}
