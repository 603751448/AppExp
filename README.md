# AppExp
组件化做一个app的架子。方便新建项目使用

## 项目结构
* project
    * app（空壳架子，用来组织组件之间展示）
    * basic（项目必须的依赖，基本上每个项目都必须的一些工具类）
        * base（baseActivity和baseFragment）
            * baseActivity
            * baseFragment
            * ToastBaseActivity(顶部弹窗的Activity)
        * dialog（常用的dialog）
            * 单选弹窗
            * 双选弹窗
        * imageLoader（图片加载库）
        * utils（工具类）
            * 权限工具类
            * 颜色工具类
            * 时间日期工具类
            * log
            * 数字工具类
            * 手机系统和硬件相关工具类
            * 尺寸工具类
            * SP存储
        * view（常用的控件）
            * actionBar
            * statusBar
            * 加载更多
                * recyclerView加载更多
            * 刷新控件（支持扩展头部）
                * 下拉刷新
                * 下拉刷新，继续下拉打开页面
            * 状态控件
                * 无数据
                * 无网络
                * 加载失败
            * 仿IOS菊花
    * copyUi（高仿Ui）
        * 炫酷的消息卡片弹出效果
        * 高仿频道管理
        * 炫酷的转场动画
    * customView（自定义控件）
        * 裁切
            * 裁切图片
        * 万物皆可弹效果(滚动到底部，接着拖拽回弹效果)
            * 弹性recyclerView
            * 弹性ViewPager
        * indicator
            * 暂时有问题
        * 图片预览
            * 别人的代码。计划自己写一遍。
        * recyclerView相关效果
        * 水平滚动的LinearLayout
        * 五个节点的toggleButton
        * viewPager相关效果
        * 仪表盘
        * 半圆（支持设置方向）
        * 线性渐变文字
        * 圆角的RelativeLayout
    * systemOpt（系统优化相关）
    * utilsExample（工具类功能展示）