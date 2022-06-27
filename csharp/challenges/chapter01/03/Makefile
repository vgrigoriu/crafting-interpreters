src = $(wildcard *.c)
obj = $(src:.c=.o)

LDFLAGS =

hello: $(obj)
	$(CC) -o $@ $^ $(LDFLAGS)

.PHONY: clean
clean:
	rm -f $(obj) hello
