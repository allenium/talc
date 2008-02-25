#!/usr/bin/python

max_count = 35

# http://en.wikipedia.org/wiki/Fibonacci_number
def fib(n):
  n0 = 0
  n1 = 1
  for i in range(0, n):
    n2 = n0 + n1
    n0 = n1
    n1 = n2
  return n0

for i in range(0, max_count):
  print i, "\t", fib(i)
exit(0)
