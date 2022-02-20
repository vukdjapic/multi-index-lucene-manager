package com.komante.lucene;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/** Represents lucene index */
@Getter
@ToString
public class Index {

	private String name;

	public Index(@NonNull String name) {
		this.name = name;
	}

}
