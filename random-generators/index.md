---
layout: page
image: dice-splash
title: Random Data Generators
---

#Random Data Generators and Builders#
Setting up fixtures - the objects and data you're testing against - for your tests can be difficult, time-consuming and error-prone
especially for legacy or unfamiliar systems.
Especially once we start writing integration style tests where our objects may be persisted, (de)serialized or sent over the wire
then things can really start to become challenging and frustrating.

* **Data items** affecting our test can potentially be all over the object graph, having to create
multiple objects correctly, each with their own fields, types and idiosyncracies just to get one field
in a particular state can be frustrating as well as a minefield.
* **Extra code** in our tests concerned with fixture details just adds noise to the test: 
it makes it harder to understand, more prone to human error and adds to the maintenance burden.
* **False-negatives:** To get valid fixtures we might well have to understand and set up other objects that our test just doesn’t care about, otherwise our tests fail for reasons unrelated to our test scenario
* **False-positives:** Tests might unwittingly pass because we’ve left fields empty that we don’t (think we) care about for the purposes of our test e.g. our test only works properly because something is null or false etc.

###For Example...###

As an example of what I'm talking about let’s imagine a simple car that we want to create for 
our test:

{% highlight java %}
public class Car {
    String name;
    String description;
    Manufacturer manufacturer;
    LocalDate dateOfManufacture;
    BigDecimal price;
    Engine engine;
}
{% endhighlight %}
And some typical test code to create one:

{% highlight java %}
public class SomeTest {
    private final LocalDate DATE_OF_MANUFACTURE = new LocalDate().minusYears(10);
    private Car car;

    @Before
    public void setUpCar() {
        car = new Car();
        car.setDateOfManufacture(DATE_OF_MANUFACTURE);
    }
}
{% endhighlight %}

For our test purposes we’re only interested in the age of our car, but you can see we’re 
already getting bogged down with concerns about the rest of the data on our car:

* if we don't populate name and description will it get persisted/serialized etc OK?
* and what the Engine?  And just what's involved with making an Engine object anyway?
* I’m not interested in price here but 
what is it anyway, GBP? USD? How many different code paths can my car end up going down because of 
different prices?
* You can imagine how quickly this complexity escalates with real code.

And on top of all this we might not even care about the car anyway, it might be a line item we want
to create for someone to buy for example.

###The Builder###

Using the Builder Pattern to create our fixtures can address some of these issues:

* All the fields in our object are pre-populated (where it makes sense to do so)
* In our test we only need to override the contents of the fields that we care about
* Use a fluent interface to create our object, hopefully expressing a lot of semantic meaning to 
a reader of the test.

{% highlight java %}
public class CarBuilder {
    String name = "SuperBadDog";
    String description = "Baddest Super Dog";
    Manufacturer manufacturer = Manufacturer.TOYOTA;
    LocalDate dateOfManufacture = new LocalDate().minusYears(10);
    BigDecimal price = new BigDecimal(20000);
    Engine engine = EngineBuilder.engineBuilder().build();
    
    private CarBuilder(){}
    
    public static CarBuilder carBuilder() {
        return new CarBuilder();
    }
    
    public CarBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public CarBuilder withDescription(String description) {
        this.description = description;
        return this;
    }
     
    public CarBuilder withManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
        return this;
    }
    
    public CarBuilder withDateOfManufacture(LocalDate date) {
        this.dateOfManufacture = date;
        return this;
    }
    
    public CarBuilder withAge(Integer age) {
        return withDateOfManufacture(new LocalDate().minusYears(age);
    }
    
    public CarBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }
    
    public CarBuilder withPrice(Integer price) {
        return withPrice(new BigDecimal(price));
    }
    
    public CarBuilder withEngine(Engine engine) {
        this.engine = engine;
        return this;
    }
    
    public Employee build(){
        Car car = new Car();
        car.setName(name);
        car.setDescription(description);
        car.setManufacturer(manufacturer);
        car.setDateOfManufacture(dateOfManufacture);
        car.setPrice(price);
        car.setEngine(engine);
        return car;
    }
}
{% endhighlight %}

This makes our test setup look a bit nicer and takes away some of the concerns we were having:

{% highlight java %}
public class SomeTest {
    private final Integer AGE = 10;
    private Car car;

    @Before
    public void setUpCar(){
        car = CarBuilder.carBuilder().withAge(AGE).build();
    }
}
{% endhighlight %}

A nice feature of builders is that you can add and overload methods to make your fixture setup 
simpler, more readable and more meaningful:

* We’ve overloaded `withPrice()` so you can supply a simple `Integer` and it will convert it to the required `BigDecimal` for you
* The `dateOfManufacture(LocalDate)` method is accompanied by an `age(Integer)` method so you can make your test code simpler and more readable by avoiding the boilerplate of massaging a `LocalDate`
* Finally you may have noticed that the `engine` field is populated initially with … an `EngineBuilder`!

###Better Data###

We’ve made our test setup a lot better with the builder but we can do better with the quality 
of the data. At the moment every test that uses our builder is going to get a Car with exactly 
the same default attributes which may create its own false-positive problem for us.

What we need is random data:

{% highlight java %}
public class CarBuilder {
    private Date dateOfManufacture = RDG.localDate(LocalDate.now().minusYears(15), LocalDate.now().minusYears(3)).next();
    private String name = RDG.string(15).next();
    private String description = RDG.string(50).next();
    private BigDecimal price = RDG.bigDecimal(50000).next();
    private Address homeAddress = AddressBuilder.addressBuilder().build();
    …
}
{% endhighlight %}

Well this looks exciting! Let’s take a look at some of this RDG (RandomDataGenerator) class:

{% highlight java %}
public class RDG {

    public static Generator<Integer> integer = integer(Integer.MAX_VALUE);

    public static Generator<Integer> integer(Integer max) {
        return new IntegerGenerator(max);
    }

    public static Generator<Integer> integer(Range<Integer> range) {
        return new IntegerGenerator(range);
    }

    public static Generator<String> string = string(30);

    public static Generator<String> string(Integer max) {
        return new StringGenerator(max);
    }
    …
}
{% endhighlight %}

It’s basically a collection of static `Generator<T>` members and some static helper and convenience methods.

Let’s look at the `Generator<T>` interface and the `IntegerGenerator` class:

{% highlight java %}
public interface Generator<T> {
    public T next();
}

class IntegerGenerator implements Generator<Integer> {

    private final Integer min;
    private final Integer max;

    IntegerGenerator(Integer max) {
        this.max = max;
        this.min = 0;
    }

    IntegerGenerator(Range<Integer> range) {
        this.min = range.lowerBound();
        this.max = range.upperBound();
    }

    @Override
    public Integer next() {
        return randomValues().randomInteger(min, max);
    }
}
{% endhighlight %}

`Generator<T>` is a simple interface with one method `next()`, expecting
implementing classes to be effectively a never-ending iterator of random values.

There are 2 constructors for the `IntegerGenerator` you either specify a maximum or a `Range`.
The `Range` is a Fyodor class containing just an upper and lower bound (it acts as a closed range,
meaning the bounds are inclusive). 
You can see the default version provided by `RDG.integer` uses `Integer.MAX_VALUE` when it creates 
it and it also provides a utility method to create your own `IntegerGenerator` with a different 
maximum or `Range` as needed.

And that is the general idea with all the other types we want to generate, the `StringGenerators`
in the snippet above from `RDG` provide much the same thing, although their internals are very different.

Fyodor has generators to construct pretty much anything you want, check out the user guide for more details.

Generating data with tighter formatting is also a useful addition for tests - here’s an email address generator:

{% highlight java %}
public class EmailAddressGenerator extends Generator<String> {
    @Override
    public String next() {
        return format("%s@%s.%s", Random.string(10).next(), Random.string(10).next(), Random.values("com", "co.uk", "gov.uk" , "org", "net").next());
    }
}
{% endhighlight %}

A postcode generator:

{% highlight java %}
public class PostcodeGenerator extends Generator<String> {
    @Override
    public String next() {
        return format("%s%s%01d %01d%s",
                random(1, "ABCDEFGHIJKLMNOPRSTUWYZ"),
                random(1, "ABCDEFGHKLMNOPQRSTUVWXY"),
                Random.integer(9).next(),
                Random.integer(9).next(),
                random(2, "ABDEFGHJLNPQRSTUWXYZ"));
    }
}
{% endhighlight %}

And a random URI generator:

{% highlight java %}
public class UriGenerator extends Generator<URI> {
    @Override
    public URI next() {
        try {
            return new URI(String.format("http://%s.%s", Random.string.next(), Random.values("com", "co.uk", "org").next()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
{% endhighlight %}