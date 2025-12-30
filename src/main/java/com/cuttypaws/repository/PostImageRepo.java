package com.cuttypaws.repository;

import com.cuttypaws.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepo extends JpaRepository<PostImage, Long> {
}
