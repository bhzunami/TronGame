ether
=====

A versatile, modular Java framework for 3D rendering and AV processing, including some specialities such as projection mapping or RTP streaming. Primarily targeted at OpenGL 3.3+ and based on LWJGL 3 (https://www.lwjgl.org).

Manifesto
---------

The goal of ether-gl is to create a versatile, modular graphics and audio/video library, which builds on modern graphics hardware APIs (primarily OpenGL, but in near future also Vulkan). 

* We aim at keeping the library slim, and do not support legacy hardware / drivers (OpenGL 3.3 is required for GL targets). 

* We aim at high performance, but since we use the library for teaching, we keep the code design and style to a certain degree academic and didactic, which sometimes leads to small compromises. 

* We aim at perfecting the APIs. Thus generally we won't hesitate to break backward compatiblity.

* Yes, we use Java, except for native code support where appropriate (e.g. native video decoding). We mainly focus on convenience and concepts that could be applied / ported to any language.

We will add further modules while working on the core library. 3D, audio, video and utility classes (all part of the core library) are quite usable. Animation and physics are planned as well.


Repository
----------

The build is now based on gradle, however still requires a bit of tweaking. The code typically runs out of the box on Mac OS X, Windows and Linux.

There are a few examples. For a larger one, refer to the demopolis sub-project, which realised an urban planning application. It comes with data and is fully usable, and currently exhibited at the Berlin Academy of the Arts.


Further Info & Contact
----------------------

For questions etc. feel free to be in touch with me (Stefan Arisona) at robot@arisona.ch


Credits
-------

Stefan Arisona & Simon Schubiger

Contributions by: Eva Friedrich, Samuel von Stachelski, Filip Schramka
