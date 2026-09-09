package com.company.groupware.repository;

import com.company.groupware.domain.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Integer> {

}

