#!/usr/bin/env bash
set -euo pipefail

case "${1:-}" in
  on) enabled=true ;;
  off) enabled=false ;;
  *) echo "Use on ou off." >&2; exit 2 ;;
esac

: "${AWS_CFN_EXECUTION_ROLE_ARN:?Cadastre a variável AWS_CFN_EXECUTION_ROLE_ARN}"
stack=fiap-tc4-dev

stack_json=$(aws cloudformation describe-stacks --stack-name "$stack" --output json)
stack_status=$(jq -r '.Stacks[0].StackStatus' <<< "$stack_json")
case "$stack_status" in
  CREATE_COMPLETE|UPDATE_COMPLETE|UPDATE_ROLLBACK_COMPLETE) ;;
  *) echo "A stack $stack precisa estar pronta. Estado atual: $stack_status." >&2; exit 1 ;;
esac

if ! jq -e '
  .Stacks[0].Parameters | map(.ParameterKey) |
  (index("NeonDbUrl") != null and index("ServicesEnabled") != null)
' <<< "$stack_json" > /dev/null; then
  echo "Publique o template com a conexao Neon antes de usar este workflow." >&2
  exit 1
fi

if ! jq -e --arg enabled "$enabled" '
  .Stacks[0].Parameters |
  map(select(.ParameterKey == "ServicesEnabled")) |
  all(.ParameterValue == $enabled)
' <<< "$stack_json" > /dev/null; then
  parameters=$(jq -c --arg enabled "$enabled" '
    .Stacks[0].Parameters | map(
      if .ParameterKey == "ServicesEnabled"
      then {ParameterKey, ParameterValue: $enabled}
      else {ParameterKey, UsePreviousValue: true}
      end
    )
  ' <<< "$stack_json")

  change_set=$(aws cloudformation create-change-set --stack-name "$stack" \
    --change-set-name "environment-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}" \
    --change-set-type UPDATE --use-previous-template --capabilities CAPABILITY_NAMED_IAM \
    --role-arn "$AWS_CFN_EXECUTION_ROLE_ARN" --parameters "$parameters" --query Id --output text)
  if ! aws cloudformation wait change-set-create-complete --change-set-name "$change_set"; then
    aws cloudformation describe-change-set --change-set-name "$change_set" \
      --query '{Status:Status,Reason:StatusReason}' --output json
    aws cloudformation delete-change-set --change-set-name "$change_set"
    exit 1
  fi
  aws cloudformation execute-change-set --change-set-name "$change_set"
  aws cloudformation wait stack-update-complete --stack-name "$stack"
fi
