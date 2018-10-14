# shortest-mountain-hikes

This collaborative project (with Mimi Aminuddin, Jack Armstrong, and Andy Bao) implements a **linear programming** (LP) model to find the shortest route to a set of mountains in Washington state while maximizing the hiking rates and satisfying certain minimum elevation gains.

## Model Itself
The objective function has the purpose of maximizing the total rating of the mountains to climb.

Three main categories of constraints:
* distance to each mountain from the starting point
* elevation gain of mountains
* hiking distance of mountains


## Variations of Our Model
1. Re-scaling the rating by using incremented powers
1. Re-scaling the rating by using exponential with the number of rating included/excluded
1. Adding new constraints of distance between selected trails to be greater than a certain value
1. Considering different starting points within WA, other than Seattle

   (Spokane, Colville, Olympia, Skykomish, and Wenatchee)
1. Changing mininum and maximum elevation gains

## Documentation
Project report is available *[here](https://hannah0n.github.io/shortest-mountain-hikes/index.html).*
