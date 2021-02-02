# Starling Account 1p Savings Challenge

Inspired by the [365 Day 1p Challenge 2021](https://forums.moneysavingexpert.com/discussion/6229222/365-day-1p-challenge-2021)
I decided to setup a savings goal in my [Starling Account](https://www.starlingbank.com/)
 and get going.

 I heard that [Monzo](https://monzo.com/) have a [IFTTT](https://ifttt.com/applets/GutUrxFv-take-on-the-1p-savings-challenge) module that automates the savings for you, and I thought that would be great.

But, Starling haven't created an IFTTT plugin to automate the savings :(.

I went googling and I found this project:

  * [Daniel Pomfret's Penny Saving Challenge using Starling Bank, Lambda and Node](https://dano.me.uk/code/penny-saving-challenge-using-starling-bank-lambda-and-node/)

I tried to get this to work, but I'm not a node dev and it failed for me at moving the money from my primary account into the savings goal.

so, as a Java developer, I thought I'd just write a little java app which I could run every day to move the money over.

as I was looking through the Starling developer docs, I found the [Github developer resources page](https://github.com/starlingbank/developer-resources) which listed an unofficial java library for calling the Starling API, so I thought I'd use that.

At this point, I knew I was going to write a java app and I like my Android phone and have created a few apps for it in the past, so I though it would be nice to have my penny savings app on my phone making it easier for me to get to every day.

I created a blank Android project and copied the java classes from [deepinspire/Simple-SDK-for-Starling-API-v2](https://github.com/deepinspire/Simple-SDK-for-Starling-API-v2/) into it.

I added code to one of the fragment's to call out and get the accounts. Then I realised that deepinspire's library just returned the json response, and I had to deserialize that into an object, so I created Objects for Account, Savings Goal, CurrencyAndAmount etc...

On Android keeping all network requests off the main thread is enforced and if you try to make a network request your app will fail with an exception.

So I looked into creating a ViewModel to hold the Starling Data which allows me to "observe" the data types for changes.

Then I added code to use Android Volley JsonObjectRequest to make a GET request to retrieve my Accounts data.

Now that that request was in place I copied the key loading code from [deepinspire/Simple-SDK-for-Starling-API-v2](https://github.com/deepinspire/Simple-SDK-for-Starling-API-v2/) into my ViewModel, set up a json file with the secrets info (client id, personal access token etc...) and ran the request.

Viola accounts data retrieved.
