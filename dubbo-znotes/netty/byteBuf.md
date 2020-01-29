- 罗列简易目录

#### 特性
1. 对比与jdk的ByteBuffer，可读可写，不用flip()。
2. 封装动态扩展功能，只要不超过Integer.MAX_VALUE即可。jdk的byteBuffer底层数组使用final修饰，没法扩容。

#### 实现
- https://www.cnblogs.com/wade-luffy/p/6196481.html
1. 实现特性1，采用3个index——readeIndex，writeIndex，capacity
2. write时，判断可用空间。扩容成2被空间，采用旧对象复制到新对象方式。

####缓冲区池化
- 对池中buf复用，在各场景中，内存表现稳定，不会流量突增时，内存波动。不会频繁申请、释放内存
- 实现：
  1. 预先申请一大块连续内存。
  1. 采用完全二叉树结构（chunk-page），page大小4 Bytes。
  
#### 内存分类
1. 非直接内存：
    分配在jvm堆内存中，分配、释放快，但是数据交换多一次： 非直接内存<——>内核态内存<——>用户态内存。todo 为啥要这么安排
1. 直接内存
    分配在jvm之外，分配、释放麻烦，数据交换可直达用户态内存。
    
#### native-transport
- 使用了epoll的另一种触发模式：edge-triggered（边沿触发）。NIO采用level-triggered（水平触发）
