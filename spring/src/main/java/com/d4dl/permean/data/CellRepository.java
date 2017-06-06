package com.d4dl.permean.data;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by joshuadeford on 6/6/17.
 */
@RepositoryRestResource
public interface CellRepository extends PagingAndSortingRepository <Cell, String>{
}
