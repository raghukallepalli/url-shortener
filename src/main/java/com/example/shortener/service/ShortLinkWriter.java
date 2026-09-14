package com.example.shortener.service;

import com.example.shortener.domain.ShortLink;
import com.example.shortener.repository.ShortLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkWriter {

	private final ShortLinkRepository shortLinkRepository;

	public ShortLinkWriter(ShortLinkRepository shortLinkRepository) {
		this.shortLinkRepository = shortLinkRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ShortLink insert(ShortLink link) {
		return shortLinkRepository.saveAndFlush(link);
	}
}