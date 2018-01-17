package com.d4dl.permean;

import com.d4dl.permean.data.Cell;
import com.d4dl.permean.data.CellRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;
import java.util.List;

@ContextConfiguration(classes = {PermeanApplication.class})
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PermeanApplicationTests {


	@Autowired
	private CellRepository cellRepository;

	@Test
	public void contextLoads() {
		//List<Cell> cells = cellRepository.findInLatLngRange(27.125300143532133, 27.16669602267853, -32.07185468292238, -31.97014531707765);
		List<Cell> cells1 = cellRepository.findByVerticesLatitudeBetweenAndVerticesLongitudeBetween(
				new BigDecimal("27.125300143532133"),
				new BigDecimal("27.16669602267853"),
				new BigDecimal("-32.07185468292238"),
				new BigDecimal("-31.97014531707765"));
		List<Cell> cells2 = cellRepository.findByVerticesLatitudeBetweenAndVerticesLongitudeBetween(
				new BigDecimal("0"),
				new BigDecimal("180"),
				new BigDecimal("0"),
				new BigDecimal("90"));
		System.out.println("Done");
	}

}
