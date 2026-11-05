package com.gabriele.iodice.services;

import com.gabriele.iodice.entities.StandardMessage;

public interface ConverterService<T> {

    StandardMessage Convert(T message);
}
