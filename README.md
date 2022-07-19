<h1> Numerology Test App for Testing</h1>

    An android app that does a numerology explanation based on this input:


<h3> Requirement </h3>

* The user chooses a number between 1 and 1000, we call this number N.

* With this input get the FIRST N decimal digits of 'e', the base of the natural logarithm

* Also get the LAST N digits of 'pi'

* Sum both numbers found, let's name this number K

* Count the occurrences of 2 digit prime numbers found in the digits of K, this will be a

* Count the occurrences of 3 digit prime numbers found in the digits of K, this will be b

* multiply a with b

* then do regular numerology addition of each digit to form a new number,

* loop until you end with a number between 0 and 9

* display the today's prediction based on the number found  



<h3> Example For Understanding </h3>

    So let's say I pick N  = 10
    then  we would have:
    7182818284  from e
    1814666323  from pi

    sum = 8997484607

    2 digit primes:  89  97  07   --> a = 3
    3 digit primes:  997  607    --> b = 2

    a * b = 6

    No need to make it denser
    So the final number is 6

    Then make it display a text similar to this one: https://www.numerology.com/articles/about-numerology/single-digit-number-6-meaning/


<h3> Assumption and Marks</h3>

 * We use the FIRST N decimal digits of 'Pi' because Pi has an infinite number of digits, so simply, there isn’t a last digit of pi.
 * Numerology numbers are 1 to 9 so final number should in between and If get '0' in answer I converted to one.
    
  
