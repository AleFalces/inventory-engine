package com.omnicore.inventory_engine;

import org.springframework.boot.SpringApplication;

public class TestInventoryEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(InventoryEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
