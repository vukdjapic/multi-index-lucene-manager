Multi Index Lucene Manager
==========================
This library was created to facilitate management of multiple Lucene indexes.

It is remotely similar to *EntityManager* for JPA. The goal was to have central registry,
or cache, of all needed Lucene indexes, with API to do usual index operations. And it is also
very thin layer above Lucene Java API, with just several public classes.

### [Igra recima - Recnik upotrebe i rima srpskih reci](http://igrarecima.com)
Multi Index Lucene Manager was developed and implemented in the project <http://igrarecima.com>, Serbian dictionary
with words usage examples and various other text and words analysis tools.

### Usage
All interaction with Lucene goes through **LuceneManager** interface. Which is
instantiated with **new DefaultLuceneManager(LuceneConfig)**, where LuceneConfig is central configuration object,
which can be created with builder. All settings have sensible default values.


