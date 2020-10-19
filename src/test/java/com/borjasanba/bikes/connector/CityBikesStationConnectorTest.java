package com.borjasanba.bikes.connector;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import com.borjasanba.bikes.client.CityBikeStationClient;
import com.borjasanba.bikes.model.Station;
import com.borjasanba.bikes.producer.CityBikesProducer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CityBikesStationConnector.class})//, BikesConfig.class})
@ImportAutoConfiguration({FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
@TestPropertySource(locations = "classpath:application.yml")
public class CityBikesStationConnectorTest {

    @Autowired
    private CityBikesStationConnector cityBikesStationConnector;

    @Autowired
    private CityBikeStationClient cityBikeStationClient;

    @MockBean
    private CityBikesProducer cityBikesProducer;

    @Rule
    public PactProviderRuleMk2 stubProvider = new PactProviderRuleMk2("bikestation-api", "localhost", 9999, this);

    @Test
    @PactVerification(fragment = "retrieveStationInfo", value = "bikestation-api")
    public void shouldProduceStationInfo() {
        // given & when
        cityBikesStationConnector.sendStationInfo();

        // then
        verify(cityBikesProducer, times(4)).sendStationInfo(any(Station.class));
    }

    @Test
    @PactVerification(fragment = "retrieveStationStatus", value = "bikestation-api")
    public void shouldProduceStationStatus() {
        // given & when
        cityBikesStationConnector.sendStationStatus();

        // then
        verify(cityBikesProducer, times(4)).sendStationStatus(any(Station.class));
    }

    @Pact(provider = "bikestation-api", consumer = "citibikenycClient")
    public RequestResponsePact retrieveStationInfo(final PactDslWithProvider builder) {
        // @formatter:off
        return builder
                .given("provider accepts get station info request")
                .uponReceiving("a request to get station info list")
                .path("/gbfs/en/station_information.json")
                .method("GET")
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json")
                .body("{\"data\": {\"stations\": [" +
                        "{\"station_id\": \"72\", \"name\": \"W 52 St & 11 Ave\", \"lat\": 40.76727216, \"lon\": -73.99392888, \"capacity\": 55}," +
                        "{\"station_id\": \"79\", \"name\": \"Franklin St & W Broadway\", \"lat\": 40.71911552, \"lon\": -74.00666661, \"capacity\": 33}," +
                        "{\"station_id\": \"82\", \"name\": \"St James Pl & Pearl St\", \"lat\": 40.71117416, \"lon\": -74.00016545, \"capacity\": 27}," +
                        "{\"station_id\": \"83\", \"name\": \"Atlantic Ave & Fort Greene Pl\", \"lat\": 40.68382604, \"lon\": -73.97632328, \"capacity\": 62}" +
                        "]}}")
                .toPact();
        // @formatter:on
    }

    @Pact(provider = "bikestation-api", consumer = "citibikenycClient")
    public RequestResponsePact retrieveStationStatus(final PactDslWithProvider builder) {
        // @formatter:off
        return builder
                .given("provider accepts get station status request")
                .uponReceiving("a request to get station status list")
                .path("/gbfs/en/station_status.json")
                .method("GET")
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json")
                .body("{\"data\": {\"stations\": [" +
                        "{\"station_id\": \"72\", \"num_bikes_available\": 12}," +
                        "{\"station_id\": \"79\", \"num_bikes_available\": 0}," +
                        "{\"station_id\": \"82\", \"num_bikes_available\": 4}," +
                        "{\"station_id\": \"83\", \"num_bikes_available\": 31}" +
                        "]}}")
                .toPact();
        // @formatter:on
    }

}
