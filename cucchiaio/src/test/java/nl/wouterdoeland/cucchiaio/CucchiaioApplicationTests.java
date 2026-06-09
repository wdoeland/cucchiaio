package nl.wouterdoeland.cucchiaio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CucchiaioApplicationTests {

    @Test
    void contextLoads() {
    }

}
