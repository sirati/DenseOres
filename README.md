DenseOres by RWTema
=========  

**Note: Dense Ores has updated to 1.12.2 If you are looking for an older version go to [rwtema original repo](https://github.com/rwtema/DenseOres)**  

You can also find individual example configs for common mods at [https://github.com/rwtema/DenseOres/tree/master/example\_configs/mods](https://github.com/rwtema/DenseOres/tree/master/example_configs/mods). This will require you to change the ore ids when you add them to your config file. If anyone wishes to add more example configs for common mods, then please feel free to submit a pull request for it.  

Config Info
=====

_S:baseBlock_ - The ore block that you wish to replace. This is in the form modid:blockname  
_I:baseBlockProperties_ - The set of properties defining the blockstate/block sub variant. As seen when looking at the block in F3  
_S:baseBlockTexture_ - The ores texture name (as found in assets/modid/textures/blocks)  
_D:denseOreProbability_ - Currently unused.  
_I:renderType_ - This changes the way the texture generation works (see [here](https://i.imgur.com/CGfhSss.png) for details).  
_I:retroGenId_ - Retrogen number. Set it to non-zero to enable retrogen. You can change it to a diffent number to run retrogen again.  
_S:underlyingBlock_ - The texture of the base block (usually stone or netherrack), see baseBlockTexture.