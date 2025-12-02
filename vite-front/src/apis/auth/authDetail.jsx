import axios from "axios";
import { BACK_BASIC_URL } from "../commonApis";
import store from "../../store/store";
import jwtAxios from "../util/jwtUtil";
import { deleteAccessToken } from "../../slices/jwtSlice";

// 회원조회
export const authDetailFn = async () => {
  const ACCESS_TOKEN_KEY = localStorage.getItem("accessToken");
  try {
    const res = await jwtAxios.get(
         `${BACK_BASIC_URL}/api/member/detail`,
      {
        headers: { Authorization: `Bearer ${ACCESS_TOKEN_KEY}` },
        withCredentials: true,
      }
    );
    return res;
  } catch (err) {
    console.log("회원 조회를 실패했습니다.");
  }
};

// 회원수정
export const authUpdateFn = async (memberDto, imgFile) => {
  const ACCESS_TOKEN_KEY = localStorage.getItem("accessToken");
  const formData = new FormData();

  formData.append(
    "memberDto",
    new Blob([JSON.stringify(memberDto)], { type: "application/json" })
  );
  formData.append("memberFile", imgFile);

  try {
    const res = await jwtAxios.put(
       `${BACK_BASIC_URL}/api/member/update`,
      formData,
      {
        headers: {
          Authorization: `Bearer ${ACCESS_TOKEN_KEY}`,
          "Content-Type": "multipart/form-data",
        },
      }
    );
    return res;
  } catch (err) {
    console.log("업데이트 오류: " + err);
  }
};

// 회원삭제
export const authDeleteFn = async (memberId) => {
  const ACCESS_TOKEN_KEY = localStorage.getItem("accessToken");
  const rs = window.confirm("정말 탈퇴하시겠습니까?");
  if (rs) {
    try {
      await jwtAxios.delete(`${BACK_BASIC_URL}/api/member/delete/${memberId}`, {
        headers: { Authorization: `Bearer ${ACCESS_TOKEN_KEY}` },
      });

      await axios.post(
        `${BACK_BASIC_URL}/api/member/logout`,
        {},
        {
          headers: {
            Authorization: `Bearer ${ACCESS_TOKEN_KEY}`,
          },
          withCredentials: true,
        }
      );

      localStorage.removeItem(ACCESS_TOKEN_KEY);
      store.dispatch(deleteAccessToken());
    } catch (err) {
      console.log("탈퇴 실패 " + err);
    }
  }
};

// app.jsx 새로고침 일어날때 재로그인 성공 후 memberDetail을 뽑아옵니다.
export const indexUserDetailFn = async (token) => {
  try {
    const res = await jwtAxios.get(
       `${BACK_BASIC_URL}/api/member/detail`,
      {
        headers: { Authorization: `Bearer ${token}` },
        withCredentials: true,
      }
    );

    return res;
  } catch (err) {
    console.log("회원 조회를 실패했습니다.");
  }
};
