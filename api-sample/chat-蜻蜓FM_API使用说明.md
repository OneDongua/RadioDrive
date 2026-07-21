### USER

探索这两个仓库，寻找蜻蜓fm的api使用方法，给出说明书
https://github.com/MyLovePoppet/FM
https://github.com/HowieHChen/PureRadio.Uwp/blob/d00fbc2393f5d0b54146292363655c29a441a2b0/PureRadio.Uwp/Models/Data/Constants/ApiConstants.cs

### ASSISTANT

基于对您提供的两个 GitHub 仓库（`MyLovePoppet/FM` 和 `PureRadio.Uwp`）的深度分析，我为您整理了一份详细的
**蜻蜓FM API 使用说明书**。

> ⚠️ **免责声明**
> ：以下接口和签名算法均通过逆向工程从开源项目中提取。官方可能会随时更新接口地址、参数或签名密钥导致失效。本文档仅供技术研究、学习交流使用，请勿用于商业或任何违反服务条款的用途。

---

# 📻 蜻蜓FM API 使用说明书

## 一、 核心机制：直播流地址动态签名算法

蜻蜓FM 的音乐电台直播流采用了动态签名机制以防止盗链。直接请求会失败，必须携带正确的 `ts` 和 `sign` 参数。

### 1. 接口地址模板

```text
https://lhttp.qingting.fm/live/{channel_id}/64k.mp3?app_id=web&ts={ts}&sign={sign}
```

### 2. 参数生成规则

| 参数             | 说明      | 生成规则                                                                                                          |
|:---------------|:--------|:--------------------------------------------------------------------------------------------------------------|
| `channel_id`   | 电台 ID   | 例如：`4847` (CNR MusicRadio)                                                                                    |
| `app_id`       | 应用标识    | 固定为字符串 `"web"`                                                                                                |
| `ts`           | 时间戳     | 当前 Unix 时间戳 + 3600秒（1小时），转换为 **16进制小写** 字符串。                                                                  |
| `path`         | 请求路径    | 固定格式：`/live/{channel_id}/64k.mp3`                                                                             |
| `encoded_path` | URL编码路径 | 使用 JavaScript `encodeURIComponent(path)` 编码，百分号编码保持大写（如 `/` 编码为 `%2F`）。签名输入必须与 JS 的编码结果完全一致。                  |
| `sign`         | 签名      | 使用 `HMAC-MD5` 算法，密钥为 `"Lwrpu$K5oP"`，对字符串 `"app_id=web&path={encoded_path}&ts={ts}"` 进行加密，结果转为 **16进制小写** 字符串。 |

### 3. Python 代码实现示例

```python
import time
import hmac
import hashlib
import urllib.parse

def get_qingting_live_url(channel_id: int) -> str:
    # 1. 构造基础路径
    path = f"/live/{channel_id}/64k.mp3"
    
    # 2. 计算 ts (当前时间 + 1小时，转为16进制小写，去掉'0x'前缀)
    ts_hex = hex(int(time.time()) + 3600)[2:].lower()
    
    # 3. 按 JavaScript encodeURIComponent 编码 path（保留大写 %2F）
    encoded_path = urllib.parse.quote(path, safe='')
    
    # 4. 构造待签名字符串
    app_id = "web"
    sign_str = f"app_id={app_id}&path={encoded_path}&ts={ts_hex}"
    
    # 5. 计算 HMAC-MD5 签名
    secret_key = "Lwrpu$K5oP"
    sign = hmac.new(
        secret_key.encode('utf-8'),
        sign_str.encode('utf-8'),
        hashlib.md5
    ).hexdigest().lower()
    
    # 6. 拼接最终 URL
    final_url = f"https://lhttp.qingting.fm{path}?app_id={app_id}&ts={ts_hex}&sign={sign}"
    return final_url

# 测试获取电台 4847 的播放地址
print(get_qingting_live_url(4847))
```

---

## 二、 API 接口大全

以下接口整理自 `PureRadio.Uwp` 项目，涵盖了电台、专辑、搜索和用户系统。

### 1. 基础域名

- Web BFF: `https://webbff.qingting.fm/www`
- 用户中心: `https://user.qingting.fm`
- 搜索服务: `https://search.qtfm.cn`
- 电台 API: `http://rapi.qingting.fm` / `https://rapi.qtfm.cn`
- 内容 API: `https://webapi.qingting.fm` / `https://i.qingting.fm`
- 音频流: `https://audio.qtfm.cn`

### 2. 电台相关 (Radio)

| 功能         | 请求方式 | 接口地址 / 说明                                                                                                       |
|:-----------|:----:|:----------------------------------------------------------------------------------------------------------------|
| **电台详情**   | GET  | `http://rapi.qingting.fm/channels/{channel_id}`                                                                 |
| **电台直播流**  | GET  | `http://ls.qingting.fm/live/{channel_id}/64k.m3u8` <br>*(注：此为备用 m3u8 格式，推荐使用第一部分带签名的 mp3 接口)*                   |
| **电台回放**   | GET  | `https://lcache.qtfm.cn/cache/{yyyyMMdd}/{channel_id}/{channel_id}_{yyyyMMdd}_{start_time}_{end_time}_24_0.aac` |
| **分类获取电台** | GET  | `http://rapi.qingting.fm/channels` (需附带分类参数)                                                                    |
| **电台排行榜**  | GET  | `https://rapi.qtfm.cn/billboards/0/{category_id}/channels`                                                      |

### 3. 有声内容/专辑相关 (Content)

| 功能         | 请求方式 | 接口地址 / 说明                                                                                                                 |
|:-----------|:----:|:--------------------------------------------------------------------------------------------------------------------------|
| **专辑详情**   | GET  | `https://webapi.qingting.fm/api/pc/channels/{channel_id}`                                                                 |
| **专辑播放列表** | GET  | `https://i.qingting.fm/capi/channel/` (需附带专辑ID和分页参数)                                                                      |
| **专辑音频播放** | GET  | `https://audio.qtfm.cn/audiostream/redirect/{channel_id}/{program_id}` <br>*(注：此接口可能需要使用密钥 `"fpMn12&38f_2e"` 进行类似直播流的签名)* |
| **分类筛选专辑** | GET  | `https://i.qingting.fm/capi/neo-channel-filter`                                                                           |

### 4. 搜索相关 (Search)

蜻蜓FM 的搜索接口采用 **GraphQL** 格式。

- **搜索建议**: `GET https://search.qtfm.cn/v3/suggest?keyword={keyword}`
- **综合搜索**: `POST/GET` 到 `https://webbff.qingting.fm/www`
    - **请求体模板** (需替换 `{0}`, `{1}`, `{2}`):
      ```json
      {
        "query": "{searchResultsPage(keyword:\"{0}\", page:{1}, include:\"{2}\") {numFound, searchData}}"
      }
      ```
    - **参数说明**:
        - `{0}`: 搜索关键字
        - `{1}`: 页码 (从 1 开始)
        - `{2}`: 搜索类型，`channel_live` (电台) 或 `channel_ondemand` (专辑/有声内容)

### 5. 用户账户相关 (Account)

| 功能           | 请求方式 | 接口地址                                            |
|:-------------|:----:|:------------------------------------------------|
| **用户登录**     | POST | `https://user.qingting.fm/u2/api/v4/user/login` |
| **刷新 Token** | POST | `https://user.qingting.fm/u2/api/v4/auth`       |
| **我的收藏**     | GET  | `https://webbff.qingting.fm/www/favchannel`     |

---

## 三、 关键密钥 (Secret Keys)

在逆向分析中发现的用于生成签名的 HMAC 密钥：

1. **电台直播流签名密钥**: `"Lwrpu$K5oP"`
2. **专辑/有声内容播放签名密钥**: `"fpMn12&38f_2e"`

---

## 四、 ⚠️ 开发与调试注意事项

1. **URL 编码必须匹配 JavaScript**：在生成 `sign` 时，`path` 必须按 `encodeURIComponent(path)`
   编码，斜杠应编码为 `%2F`（大写 `F`）。不要将编码结果转为小写；`%2f` 与 `%2F` 会产生不同的 HMAC
   签名并导致校验失败（HTTP 403）。
2. **时间戳容错**：`ts` 参数是“当前时间 + 1小时”，服务器会校验该时间戳的有效性，过期或偏差过大的请求会被拒绝。
3. **请求头 (Headers)**：在调用 Web BFF 或用户接口时，建议带上常规的浏览器 Request Headers（如
   `User-Agent`, `Referer: https://www.qingting.fm/`），以降低被风控拦截的概率。
4. **接口时效性**：以上 API 基于 2021-2022 年间的开源项目提取。若遇到 `404 Not Found` 或
   `403 Forbidden`，大概率是官方已升级接口或更换了密钥，需要重新抓包分析。

如需进一步探索某个特定接口（如专辑播放的具体签名逻辑），可以使用浏览器的 Developer Tools (F12)
在 [蜻蜓FM网页版](https://www.qingting.fm) 播放音频时，拦截 Network 面板中的请求进行对比验证。

### USER

webbff有哪些用法

### ASSISTANT

基于对开源仓库（`PureRadio.Uwp` 和 `8DE4732A/fm` 等）的深度逆向分析，**`webbff`**（Backend For
Frontend）是蜻蜓FM专门为 Web 端和轻量级客户端设计的数据聚合网关。

与传统的 RESTful API 不同，`webbff` 主要采用 **GraphQL** 协议，允许客户端在一次请求中精确获取所需的嵌套数据，从而减少网络请求次数。

以下是 `webbff` 的详细使用说明书：

---

### 一、 基础通信规范

- **基础域名**：`https://webbff.qingting.fm/www` （备用域名：`https://webbff.qtfm.cn/www`）
- **请求方法**：`POST`
- **Content-Type**：`application/json`
- **请求体格式**：必须包含 `query` 字段，值为 GraphQL 查询语句字符串。
  ```json
  {
    "query": "{ 你的 GraphQL 查询语句 }"
  }
  ```

---

### 二、 核心 GraphQL 查询场景

#### 1. 获取地区分类及电台列表 (最常用)

蜻蜓FM的直播电台按“省/市”地区分类。获取电台需要分两步：先获取地区列表，再根据地区 ID 获取电台。

- **步骤 A：获取所有地区列表**
  *(注：`cid: 432` 是蜻蜓FM内部的根分类 ID，代表所有地区)*
  ```graphql
  {
    radioPage(cid: 432, page: 1) {
      bannerData
      regions { id, title }
      radioPlaying
      replayRadio
      classes
    }
  }
  ```
  *返回结果中的 `regions` 数组包含了所有地区的 `id` 和 `title`（如：北京、上海）。*

- **步骤 B：根据地区 ID 获取该地区的电台列表**
  *(将 `{地区ID}` 替换为步骤 A 中获取的真实 ID，例如北京可能是某个特定数字)*
  ```graphql
  {
    radioPage(cid: {地区ID}, page: 1) {
      contents {
        items {
          id
          title
          imgUrl
          desc
          playcount
        }
      }
    }
  }
  ```

#### 2. 首页/电台推荐数据

用于获取首页的“正在播放”、“回放电台”或首页推荐电台聚合数据。

```graphql
{
  radioPage {
    radioPlaying
    replayRadio
  }
}
```

或获取主页电台数据：

```graphql
{
  homePage {
    radioData
  }
}
```

#### 3. 综合搜索 (电台 或 有声内容)

支持搜索直播电台 (`channel_live`) 或有声内容/专辑 (`channel_ondemand`)。

- **GraphQL 查询模板**：
  ```graphql
  {
    searchResultsPage(keyword: "{关键字}", page: {页码}, include: "{搜索类型}") {
      numFound
      searchData
    }
  }
  ```
- **参数说明**：
    - `keyword`：搜索关键字（在 JSON 中需转义为 `\"关键字\"`）
    - `page`：页码，从 `1` 开始
    - `include`：固定为 `"channel_live"` (搜电台) 或 `"channel_ondemand"` (搜专辑/有声书)

#### 4. 有声内容分类推荐

用于获取有声书、相声评书等频道的首页分类推荐。

```graphql
{
  homePage {
    categories
  }
}
```

---

### 三、 关联的 RESTful 接口 (同域名下)

除了 GraphQL，`webbff` 域名下还保留了少量传统的 RESTful 接口，主要用于用户个人数据：

| 功能          |  方法   | 接口地址                                        | 说明                                       |
|:------------|:-----:|:--------------------------------------------|:-----------------------------------------|
| **获取用户收藏夹** | `GET` | `https://webbff.qingting.fm/www/favchannel` | 需在 Header 中携带有效的用户登录凭证 (Cookie 或 Token)。 |

---

### 四、 代码调用示例 (Python)

以下是一个完整的搜索功能调用示例，展示了如何构造 GraphQL 请求：

```python
import requests
import json

def search_qingting(keyword: str, search_type: str = "channel_live", page: int = 1):
    url = "https://webbff.qtfm.cn/www"
    
    # 构造 GraphQL 查询语句，注意关键字的双引号需要转义
    query = f'{{searchResultsPage(keyword:"{keyword}", page:{page}, include:"{search_type}") {{numFound, searchData}}}}'
    
    headers = {
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer": "https://www.qingting.fm/"
    }
    
    payload = {
        "query": query
    }
    
    response = requests.post(url, headers=headers, json=payload)
    response.raise_for_status()
    return response.json()

# 示例 1：搜索“音乐”相关的直播电台
radio_result = search_qingting("音乐", search_type="channel_live", page=1)
print("电台搜索结果:", json.dumps(radio_result, indent=2, ensure_ascii=False))

# 示例 2：搜索“郭德纲”相关的有声专辑
content_result = search_qingting("郭德纲", search_type="channel_ondemand", page=1)
print("专辑搜索结果:", json.dumps(content_result, indent=2, ensure_ascii=False))
```

---

### 五、 ⚠️ 开发与调试注意事项

1. **GraphQL 语法严格**：查询语句中的大括号 `{}`、双引号 `""` 必须严格匹配。如果在 JSON
   中直接拼接字符串，务必注意转义（如 `\"`）。建议使用 Postman 或 Apifox 的 GraphQL 模式先进行调试。
2. **字段变更风险**：BFF 层的 GraphQL Schema 会随官方前端版本迭代而微调（如增加或废弃某些返回字段）。若遇到
   `Cannot query field "xxx" on type "yyy"` 错误，请打开浏览器 F12，在蜻蜓FM官网执行相同操作，抓取
   Network 面板中最新的 `query` 语句进行替换。
3. **反爬虫与风控**：高频请求可能会触发 WAF 拦截。建议在请求头中携带真实的 `User-Agent` 和 `
   Referer: https://www.qingting.fm/`，并在爬虫逻辑中加入适当的请求延迟。
4. **配合播放接口使用**：`webbff` 返回的电台列表中会包含电台的 `id`。获取到 `id` 后，需结合前一份说明书中的
   **HMAC-MD5 动态签名算法**，才能生成最终可播放的 `.mp3` 或 `.m3u8` 流媒体地址。
