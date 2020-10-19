package com.borjasanba.bikes.service;

import com.borjasanba.bikes.config.BikesConfig;
import com.borjasanba.bikes.model.StationStat;
import com.borjasanba.bikes.model.Station;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CityBikesService.class, BikesConfig.class})
public class CityBikesServiceTest {

    private static final String BIKE_STATION_STATUS_TOPIC = "station_status";
    private static final String BIKE_STATION_INFO_TOPIC = "station_info";
    private static final String LOW_AVAILABILITY_TOPIC = "low_availability";

    @Autowired
    private CityBikesService cityBikesService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CityBikesServiceProcessor cityBikesServiceProcessor;

    private TopologyTestDriver testDriver;

    private ConsumerRecordFactory<String, String> recordFactory;

    @Before
    public void setUp() {
        recordFactory = new ConsumerRecordFactory<>(Serdes.String().serializer(), Serdes.String().serializer());
        StreamsBuilder builder = new StreamsBuilder();
        KTable<String, String> stationInfo = builder.table(BIKE_STATION_INFO_TOPIC);
        KStream<String, String> stationStatus = builder.stream(BIKE_STATION_STATUS_TOPIC);
        KStream<String, String> lowAvailability = cityBikesService.process(stationStatus, stationInfo);
        lowAvailability.to(LOW_AVAILABILITY_TOPIC, Produced.with(Serdes.String(), Serdes.String()));
        testDriver = new TopologyTestDriver(builder.build(), getStreamsConfiguration());
    }

    @After
    public void finish() {
        Try.run(() -> testDriver.close());
    }

    private Properties getStreamsConfiguration() {
        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "low-availability-id-test");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9999");
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, "target");
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");
        return streamsConfiguration;
    }

    @Test
    public void shouldProduceLowAvailabilityWhenBelowThreshold() throws JsonProcessingException {
        // given
        String key = "72";
        Station infoValue = Station.builder()
                .stationId("72")
                .name("W 52 St & 11 Ave")
                .latitude(40.76727216)
                .longitude(-73.99392888)
                .capacity(55)
                .build();
        Station statusValue = Station.builder()
                .stationId("72")
                .numBikesAvailable(1)
                .build();

        // when
        testDriver.pipeInput(recordFactory.create(BIKE_STATION_INFO_TOPIC, key, objectMapper.writeValueAsString(infoValue),
                Timestamp.valueOf(LocalDateTime.now()).getTime()));
        testDriver.pipeInput(recordFactory.create(BIKE_STATION_STATUS_TOPIC, key, objectMapper.writeValueAsString(statusValue),
                Timestamp.valueOf(LocalDateTime.now()).getTime()));

        // then
        final ProducerRecord<String, String> lowAvailabilityRecord = readFromTopic(LOW_AVAILABILITY_TOPIC);
        assertThat(lowAvailabilityRecord).isNotNull();
        assertThat(lowAvailabilityRecord.key()).isEqualTo("72");
        final StationStat stationStat = objectMapper.readValue(lowAvailabilityRecord.value(), StationStat.class);
        assertThat(stationStat)
                .extracting("name", "availabilityRatio", "latitude", "longitude")
                .containsExactly("W 52 St & 11 Ave", 1.8181819f, 40.76727216, -73.99392888);
    }

    @Test
    public void shouldNotProduceLowAvailabilityWhenAboveThreshold() throws JsonProcessingException {
        // given
        String key = "72";
        Station infoValue = Station.builder()
                .stationId("72")
                .name("W 52 St & 11 Ave")
                .latitude(40.76727216)
                .longitude(-73.99392888)
                .capacity(55)
                .build();
        Station statusValue = Station.builder()
                .stationId("72")
                .numBikesAvailable(15)
                .build();

        // when
        testDriver.pipeInput(recordFactory.create(BIKE_STATION_INFO_TOPIC, key, objectMapper.writeValueAsString(infoValue),
                Timestamp.valueOf(LocalDateTime.now()).getTime()));
        testDriver.pipeInput(recordFactory.create(BIKE_STATION_STATUS_TOPIC, key, objectMapper.writeValueAsString(statusValue),
                Timestamp.valueOf(LocalDateTime.now()).getTime()));

        // then
        final ProducerRecord<String, String> lowAvailabilityRecord = readFromTopic(LOW_AVAILABILITY_TOPIC);
        assertThat(lowAvailabilityRecord).isNull();
    }

    private ProducerRecord<String, String> readFromTopic(final String topic) {
        return testDriver.readOutput(topic, Serdes.String().deserializer(), Serdes.String().deserializer());
    }

}
