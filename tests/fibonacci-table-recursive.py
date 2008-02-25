#!/usr/bin/python

max_count = 35

# http://en.wikipedia.org/wiki/Fibonacci_number
def fib(n):
  if n < 2:
    return n
  else:
    return fib(n - 1) + fib(n - 2)

for i in range(0, max_count):
  print i, "\t", fib(i)
exit(0)
