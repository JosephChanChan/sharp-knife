# sharp-knife

**帮助编写高性能的多任务并行调度的框架，简单易使用**

> **The framework help you coding a high-performance program easily**

### 为什么会有这个框架

  在电商业务中在首页或商品列表页、商详页需要展示大量的信息，在如今微服务、分布式的架构设计下，通常完成一个页面的渲染请求需要扩散出N个读取请求。每一个读取请求都可能要通过RPC或Rest接口
调用远程服务获取数据，而这些接口获取的数据之间又有前后依赖关系，有一些又没有。为了提高请求的处理速度自然会使用线程池的方式。普通线程池无法做到任务的编排和自动触发，无法对每个任务的执行结果
进行通知和使用。

  > 此框架是 *国内头部二手电商公司转转，内部并行任务调度框架的雏形* 。相同的解决思路，不同的代码编写。
  
  > 转转卖场，是电商业务中的服务聚合层，使用此类解决方案成功经受618、双十一等严苛流量洪峰考验。

  sharp-knife帮助解决复杂业务场景下多任务需要并行执行和有依赖顺序时，简单高效地编写并行处理程序。
  

### 使用场景

#### 复杂的任务任意编排场景
  
  > 举例：转转卖场商品列表

在电商业务中，由于复杂的业务信息和微服务架构设计下，商品列表页要展示的信息非常多，背后需要处理的流程也是纷繁复杂。
举例其中的重要信息:

1. 根据策略，调搜索或推荐服务，获取根据商品ID信息
2. 通过商品ID获取商品基础、扩展信息
3. 获取商品属性、标签信息
4. 获取商品优惠券信息
5. 获取用户个性化业务信息
6. ...

往往还需要根据前面接口获取的信息决定下一步走什么策略或要不要执行逻辑，也就是说接口调用之间有依赖关系。如何处理有依赖关系的接口，让其按照工程师预期的顺序执行。以及并行请求无依赖关系的
接口，以便提高流程的处理速度。其实这就像是一副有向无环图，每个接口或计算逻辑可以封装成一个任务节点，有依赖关系和没有依赖的节点编排起来，交给框架自动调度并行执行。

例如下图的编排场景，是转转卖场商列中简化的任务链路图

<img src="https://user-images.githubusercontent.com/19208259/179020912-4ef13360-c316-4566-84a6-19d62632849c.png" width="680px"/>


### 最佳实践

- Maven坐标

```
<dependency>
  <groupId>com.joseph</groupId>
  <artifactId>sharp-knife.blade</artifactId>
  <version>0.1-SNAPSHOT</version>
</dependency>
```

- spring配置

  在application.yml中加入以下配置。未来扩展的其它配置待补充..

  ```
  sharp.knife:
    ## 全局配置，全局配置优先级 < 用户配置
    global:
      ## 任务节点超时时间
      timeoutPerTask: 200
      ## 任务链路图总的超时时间，超过时间主线程会唤醒并且直接中断剩余未执行的任务节点
      timeoutTotal: 10000
      ## 开启总的超时等待事件，否则主线程阻塞等待直到任务链路图执行完毕
      enableTimeoutTotal: true
  ```


- sharp-knife中的类

  1. `ScheduleRequest`类是面向用户的，封装用户请求的对象

  2. `ConcurrentScheduler`类是框架的核心接口，并行执行器由它接收用户提交的`ScheduleRequest`并开始执行对应的任务链路图

  3. `TaskType`抽象类，代表了一系列任务节点的身份特征。用户自定义子类继承并实现它，在任务节点类上声明此类bean名称，完成任务链路图的聚合构建

  4. `Ctx`泛型，执行过程上下文对象，保存任务链路图执行过程中的中间信息。中间信息应尽快使用、计算，将结果输出到`Res`对象中，以便尽快GC，减轻JVM压力

  5. `Res`泛型，执行结果对象，保存任务节点执行完成的信息，最终用户可拿到`Res`对象作为请求的执行结果

  6. `ExecutionResult`类是框架的执行结果对象，封装了执行过程中的错误信息（若有），用户通过它判断是否执行中出错

  7. `TaskConfig`注解是任务的基本配置信息，对任务节点进行命名，声明归属的`TaskType`，以及该节点的超时时间监控


- 示例代码

  sharp-knife是依托于Spring上运行的，所以使用方法和配置和Spring生态的其它组件相似

  #### 建立线程池
  
  将工程中用到的所有线程池在专门的配置类中定义，方便管理。使用`ThreadPoolBuilder`类建造合适参数的线程池对象。

  建议每个不同类型的任务链路图使用专属的线程池，隔离任务间的互相影响。

  ```
  @Configuration
  public class ThreadPoolConfig {
      @Bean
      public MonitoredThreadPool commonThreadPool() {
          ThreadPoolBuilder builder = new ThreadPoolBuilder(
                  ThreadPoolEnums.DEFAULT,
                  "commonThreadPool",
                  6,
                  36,
                  60,
                  TimeUnit.SECONDS,
                  new DefaultMonitoredQueue<>(10),
                  null,
                  null
          );
          return ThreadPoolFactory.build(builder);
      }
      @Bean
      public MonitoredThreadPool productListThreadPool() {
          ThreadPoolBuilder builder = new ThreadPoolBuilder(
                  ThreadPoolEnums.DEFAULT,
                  "productListThreadPool",
                  32,
                  32,
                  60,
                  TimeUnit.SECONDS,
                  new DefaultMonitoredQueue<>(10),
                  null,
                  null
          );
          return ThreadPoolFactory.build(builder);
      }
  }
  ```
  
  #### 定义任务身份特征
  
  框架依靠`TaskType`类寻找并聚合对应的任务节点。用户需自定义好子类继承`TaskType`类并提供相应信息
  
  ```
  @Configuration
  public class ProductListType extends TaskType {
      @Autowired
      private MonitoredThreadPool productListThreadPool;

      @Override
      public String getTypeName() {
          return "ProductListType";
      }

      @Override
      public MonitoredThreadPool getExecutorPool() {
          return productListThreadPool;
      }
  }
  ```
  
  #### 编写任务节点
  
  可以按业务需要自由定义需要并行执行的任务节点，这可能包括：耗时的RPC、重的计算逻辑等。任务节点对象实现`TaskNode`类并实现其中的必要方法
  
  自定义一个对象，声明@Component注解加入Spring容器，若任务节点由前继节点用@DependsOn注解声明依赖的前继节点名称
  
  框架会自动调度前继节点执行完毕后才尝试执行当前节点（若当前节点入度为0时立即执行）
  
  声明@TaskConfig注解，给任务节点配置基本信息：任务名称、任务类型、节点超时时间监控（超时后会记录并上报，不会自动中断线程执行）
  
  实现`TaskNode`接口时需要声明2个泛型`Ctx`和`Res`，如果不需要则可以声明Void对象
  
  每个TaskNode子类需要实现接口：
  
  - getPrediction
  
    提供断言函数，框架在执行任务前会先获取用户提供的断言函数，并将`Ctx`和`Res`对象传入供程序判断是否需要执行该节点。根据返回结果true会立即执行，false则丢弃任务
    
  - getTask
    
    提供需要执行的逻辑代码
    
  - onSuccess

    任务节点执行完毕后，框架进行回调通知

  - onError

    任务节点执行过程中出现异常，进行回调通知，并且将异常对象记录
  
  ```
  @Slf4j
  @Component
  @DependsOn("productIdAfterNode")
  @TaskConfig(taskName = "productExtInfoNode", taskType = "productListType", timeout = 100)
  public class ProductExtInfoNode implements TaskNode<ProductListContext, ProductListResult> {

      @Override
      public BiPredicate<ProductListContext, ProductListResult> getPrediction() {
          return ((productListContext, productListResultExecutionResult) -> {return true;});
      }

      @Override
      public BiConsumer<ProductListContext, ProductListResult> getTask() {
          return (context, result) -> {
              // 此处编写业务逻辑
              int millis = ThreadKit.boundMillis(105);
              log.info("productExtInfoNode task will cost={}", millis);
              ThreadKit.sleep(millis);
              result.finishNode("productExtInfoNode", result.getOrder().getAndIncrement(),
                      new String[]{
                              "productIdAfterNode"
                      });
          };
      }

      @Override
      public void onSuccess(ProductListContext executionContext, ProductListResult executionResult) {
          log.info("productExtInfoNode done");
      }

      @Override
      public void onError(ProductListContext executionContext, ProductListResult executionResult, Exception e) {
          log.error("productExtInfoNode error", e);
      }
  }
  ```
  
  
  #### 提交请求
  
  编写完任务节点后，这一系列任务节点就组成一副有向无环图，向框架提交请求对象执行这幅图
  
  注入`TaskType`对象，`ConcurrentScheduler`任务调度器
  
  ```
  @Slf4j
  @Service
  public class ProductListService {

      @Autowired
      private TaskType productListType;

      @Autowired
      private ConcurrentScheduler concurrentScheduler;

      public void test() {
          ProductListContext context = new ProductListContext();
          ProductListResult result = new ProductListResult();
          // 封装请求对象
          ScheduleRequest<ProductListContext, ProductListResult> request = new ScheduleRequest<>(productListType, context, result);
          // 提交请求，开始同步执行
          ExecutionResult executionResult = concurrentScheduler.scheduleSyn(request);
          // 判断执行结果，中间是否发生异常
          if (executionResult.hasError()) {
              throw new RuntimeException("ProductListService execute error", executionResult.getError());
          }
          // 使用ProductListResult对象的结果信息
          log.info("ProductListService done, res={}", result.sort());
      }
  }
  ```



### 未来要做的事儿

### 测试的工程

### 问题反馈

