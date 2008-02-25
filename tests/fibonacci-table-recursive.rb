#!/usr/bin/ruby -w

$max_count = 35

# http://en.wikipedia.org/wiki/Fibonacci_number
def fib(n)
  if n < 2
    return n
  else
    return fib(n - 1) + fib(n - 2)
  end
end

(0...$max_count).each() {
  |i|
  print("#{i}\t#{fib(i)}\n")
}
exit(0)
