#!/usr/bin/ruby -w

$max_count = 35

# http://en.wikipedia.org/wiki/Fibonacci_number
def fib(n)
  n0 = 0
  n1 = 1
  (0...n).each() {
    |i|
    n2 = n0 + n1
    n0 = n1
    n1 = n2
  }
  return n0
end

(0...$max_count).each() {
  |i|
  print("#{i}\t#{fib(i)}\n")
}
exit(0)
