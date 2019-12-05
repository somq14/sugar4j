CC=clang
CFLAGS=-O2 -fPIC -std=c99 -Wall -Wextra -Wpedantic
LFLAGS=-ldl

all: libipasir4j.so.1.0

libipasir4j.so.1.0: ipasir4j.o
	$(CC) -shared -Wl,-soname=libipasir4j.so.1 -o $@ $< $(LFLAGS)

ipasir4j.o: ipasir4j.c ipasir4j.h
	$(CC) -c $(CFLAGS) -I. $< $(LFLAGS)

clean:
	rm *.o
	rm lib*.so*
