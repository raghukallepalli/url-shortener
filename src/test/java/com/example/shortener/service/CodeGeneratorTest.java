package com.example.shortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

class CodeGeneratorTest {

	@Test
	void generatesCodeWithRequestedLength() {
		CodeGenerator generator = new CodeGenerator(new SecureRandom());

		assertEquals(12, generator.generate(12).length());
	}

	@Test
	void generatesOnlyBase62Characters() {
		CodeGenerator generator = new CodeGenerator(new SecureRandom());

		String code = generator.generate(128);

		assertTrue(code.matches("[A-Za-z0-9]+"));
	}

	@Test
	void rejectsLengthBelowOne() {
		CodeGenerator generator = new CodeGenerator(new SecureRandom());

		assertThrows(IllegalArgumentException.class, () -> generator.generate(0));
	}

	@Test
	void usesSecureRandomProvidedToConstructor() {
		SequenceSecureRandom secureRandom = new SequenceSecureRandom();
		CodeGenerator generator = new CodeGenerator(secureRandom);

		assertEquals("zzzz", generator.generate(4));
		assertEquals(4, secureRandom.getCalls());
	}

	private static final class SequenceSecureRandom extends SecureRandom {

		private int calls;

		@Override
		public int nextInt(int bound) {
			calls++;
			return bound - 1;
		}

		int getCalls() {
			return calls;
		}
	}
}