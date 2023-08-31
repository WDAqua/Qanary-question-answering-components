# How To Access Information On Existing Graphs

There are currently three methods supported to access information on an existing graph:
* Re-use the existing graph
* Reference an existing graph
* Copy values from an existing graph into the current graph

In the following, each Method is explained in more detail.


## Re-use an Existing Graph

With this approach, the information for new questions is added to the existing graph. 
This makes all data easily available in only one named graph. 

However, this also means that it may be more difficult to distinguish new data entries from
existing data.


### How to use this approach

t.b.d

make a post request to /questionanswering
and specify the `graph` parameter (together with other params)
but don't specify text question? 
(link to swagger)


## Reference an Existing Graph

This approach offers a clear differentiation between existing and new Data. 
However, it might be required to adjust individual component implementations to 
make use of the provided reference, reducing their reuseability and removing one of the 
core strengths of Qanary components. 

This approach may be viable, if information from an existing graph is only used in very few
components, that are specifically designed with this functionality in mind.

#### How to use this approach

Specify the parameter `priorConversation` as part of the POST request
to start a Qanary pipeline process. 
This reference to a graph uri is then available as an annotation 
to the current question `qa:Question` under the attribute `qa:priorConversation`.

```bash
$ curl http://localhost:8080/startquestionansweringwithtextquestion \
-d "question=<question>" \
-d "componentlist[]=<component" \ 
-d "priorConversation=<existingGraph"
```


## Copy Values from an Existing Graph

This approach is especially useful, if some components need a direct reference to data
that was previously computed and that is highly relevant for context-awareness. 

For Example, in the case of a chatbot that needs to answer a follow-up question that relates to 
a resource that was only specifically mentioned in a prior question.

However, note that copying *all* data over to the current graph 
will result in *more than one* question actively being held in the 
context of the current graph, which need to be properly distinguised in order to avoid
processing erorrs.

### How to use this approach

There are two ways to implement this approach, using the provided Qanary component.

**Option 1: Copy data independently of the Qanary Pipeline**

Once started, the Qanary component `qanary-component-CopyValuesOfPriorGraph` will provide 
the endpoint `/copyvaluestograph`, requiring the following parameters:
* `sourceGraph` - The existing graph with information that should be copied
* `targetGraph` - The process graph that should be updated with the existing data

You may directly formulate a POST request, or use the provided minimal web UI under 
the same endpoint. 

```bash
$ curl http://localhost:5555/copyvaluestograph \
-d "sourceGraph=<sourceGraph>" \
-d "targetGraph=<targetGraph>"
```

**Option 2: Copy data as a processing step of the Qanary Pipeline**

Alternatively, you can specify the `priorConversation` parameter when starting a Qanary
question answering process, as mentioned above. 

The component will then copy data from this existing graph into the current graph of the
Qanary process. For this, the component should be the *first in the component list* 
to be called!
