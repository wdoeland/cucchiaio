package nl.wouterdoeland.cucchiaio;

import org.springframework.boot.SpringApplication;

public class TestCucchiaioApplication {

    public static void main(String[] args) {
        SpringApplication.from(CucchiaioApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
