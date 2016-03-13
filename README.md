## FlashAndFurious

This is our car agent for the [FastAndFurious challenge]([https://github.com/FastAndFurious/Documentation) at [Start Hack](http://starthack.ch) 2016 (ranked first on finals).

> Imagine a racing car equipped with motion sensors. Imagine sensor readings pouring into your computer at a frequency of 50 Hz. With only those sensor readings, will you be able to learn what the track actually looks like? Will you be able to write a program that can learn what the track looks like? And will that program then be able to learn how to actually drive the fastest round, considering the enforced speed limits in the corners? In this exciting challenge, you'll eventually understand what it means when the Internet of Things meets Machine Learning. By applying asychronous algorithms, put together in a Reactive Programming style, you can win the challenge.

### Run on the simulator

Install and run an instance of [RabbitMQ](https://www.rabbitmq.com) on localhost.

Download the track simulator [here](https://github.com/FastAndFurious/AkkaStarterKit) and run the commands below:
```bash
$ mvn clean install
$ java -jar target/fnf.starterkit-1.0-SNAPSHOT.jar -f simulator -p rabbit
```

Only use the simulator as everything else has been rewriting in Scala inside our project. Download this repository and run:
```bash
$ sbt run
```

Go to `localhost:8081` and play with the simulation (press "Start Race").


